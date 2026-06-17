package com.morphingcoffee.gamelauncher.core.network

import java.io.File

internal object MacGameSupport {
    fun isMacOs(): Boolean {
        val os = System.getProperty("os.name").lowercase()
        return "mac" in os || "darwin" in os
    }

    fun macAppBundle(executable: File): File? {
        val marker = ".app/Contents/MacOS/"
        val path = executable.absolutePath
        val index = path.indexOf(marker)
        if (index < 0) return null
        return File(path.substring(0, index + 4))
    }

    fun prepareInstall(
        gameDir: File,
        executable: File,
    ) {
        if (!isMacOs()) return
        ensureMacOsExecutables(gameDir, executable)
    }

    fun prepareLaunch(executable: File) {
        if (!isMacOs()) return
        ensureExecutable(executable)
    }

    fun launchCommand(
        gameDir: File,
        executable: File,
    ): ProcessBuilder {
        val appBundle =
            macAppBundle(executable)
                ?: error("macOS launch requires an .app bundle path, got ${executable.path}")
        return ProcessBuilder("open", "-n", appBundle.absolutePath)
            .directory(gameDir)
    }

    private fun ensureMacOsExecutables(
        gameDir: File,
        executable: File,
    ) {
        ensureExecutable(executable)
        gameDir
            .walkTopDown()
            .filter { it.isFile && "/Contents/MacOS/" in it.absolutePath.replace('\\', '/') }
            .forEach { ensureExecutable(it) }
    }

    private fun ensureExecutable(file: File) {
        if (!file.isFile) return
        if (!file.canExecute()) {
            file.setExecutable(true, false)
        }
    }
}
