package com.morphingcoffee.gamelauncher.core.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WindowsMsiInstallMatcherTest {
    @Test
    fun programFilesPath_detectsMsiWithoutRegistry() {
        assertTrue(
            WindowsMsiInstallMatcher.isWindowsMsiInstall(
                executablePath = """C:\Program Files\GameLauncher\GameLauncher.exe""",
                expectedDisplayName = LauncherInstallIdentity.WINDOWS_MSI_DISPLAY_NAME_PROD,
                uninstallEntries = emptyList(),
            ),
        )
    }

    @Test
    fun customInstallPath_matchesRegistryInstallLocation() {
        assertTrue(
            WindowsMsiInstallMatcher.isWindowsMsiInstall(
                executablePath = """D:\Games\GameLauncher\app\GameLauncher.exe""",
                expectedDisplayName = LauncherInstallIdentity.WINDOWS_MSI_DISPLAY_NAME_PROD,
                uninstallEntries =
                    listOf(
                        WindowsUninstallEntry(
                            displayName = "GameLauncher",
                            installLocation = """D:\Games\GameLauncher""",
                            windowsInstaller = true,
                            uninstallString = """MsiExec.exe /X{GUID}""",
                        ),
                    ),
            ),
        )
    }

    @Test
    fun customInstallPath_ignoresOtherProductRegistryEntries() {
        assertFalse(
            WindowsMsiInstallMatcher.isWindowsMsiInstall(
                executablePath = """D:\Games\GameLauncher\app\GameLauncher.exe""",
                expectedDisplayName = LauncherInstallIdentity.WINDOWS_MSI_DISPLAY_NAME_PROD,
                uninstallEntries =
                    listOf(
                        WindowsUninstallEntry(
                            displayName = "GameLauncherDev",
                            installLocation = """D:\Games\GameLauncher""",
                            windowsInstaller = true,
                        ),
                    ),
            ),
        )
    }

    @Test
    fun portableExtract_notDetectedAsMsi() {
        assertFalse(
            WindowsMsiInstallMatcher.isWindowsMsiInstall(
                executablePath = """D:\Portable\GameLauncher-0.0.1-build42\GameLauncher.exe""",
                expectedDisplayName = LauncherInstallIdentity.WINDOWS_MSI_DISPLAY_NAME_PROD,
                uninstallEntries = emptyList(),
            ),
        )
    }

    @Test
    fun registryMatchWithoutInstallLocation_requiresMsiUninstallString() {
        assertTrue(
            WindowsMsiInstallMatcher.isWindowsMsiInstall(
                executablePath = """D:\Games\GameLauncher\GameLauncher.exe""",
                expectedDisplayName = LauncherInstallIdentity.WINDOWS_MSI_DISPLAY_NAME_PROD,
                uninstallEntries =
                    listOf(
                        WindowsUninstallEntry(
                            displayName = "GameLauncher",
                            windowsInstaller = true,
                        ),
                    ),
            ),
        )

        assertFalse(
            WindowsMsiInstallMatcher.isWindowsMsiInstall(
                executablePath = """D:\Games\GameLauncher\GameLauncher.exe""",
                expectedDisplayName = LauncherInstallIdentity.WINDOWS_MSI_DISPLAY_NAME_PROD,
                uninstallEntries =
                    listOf(
                        WindowsUninstallEntry(
                            displayName = "GameLauncher",
                            windowsInstaller = false,
                            uninstallString = "C:\\fake-uninstaller.exe",
                        ),
                    ),
            ),
        )
    }
}
