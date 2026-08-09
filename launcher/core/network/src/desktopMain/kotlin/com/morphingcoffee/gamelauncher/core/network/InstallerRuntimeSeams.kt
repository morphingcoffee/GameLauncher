package com.morphingcoffee.gamelauncher.core.network

import java.awt.Desktop
import java.io.File
import kotlin.system.exitProcess

/** Starts OS processes; tests capture args instead of launching real binaries. */
fun interface ProcessLauncher {
    fun start(
        command: List<String>,
        workingDirectory: File?,
        redirectErrorStream: Boolean,
    ): Process
}

object RealProcessLauncher : ProcessLauncher {
    override fun start(
        command: List<String>,
        workingDirectory: File?,
        redirectErrorStream: Boolean,
    ): Process {
        val builder = ProcessBuilder(command)
        if (workingDirectory != null) {
            builder.directory(workingDirectory)
        }
        if (redirectErrorStream) {
            builder.redirectErrorStream(true)
        }
        return builder.start()
    }
}

/** Process exit handoff for MSI/DMG updater flows; tests must never terminate the JVM. */
fun interface ProcessExiter {
    fun exit(status: Int)
}

object RealProcessExiter : ProcessExiter {
    override fun exit(status: Int) {
        exitProcess(status)
    }
}

/** Desktop / shell reveal actions for update artifact handoff. */
interface DesktopActions {
    fun isDesktopSupported(): Boolean

    fun open(file: File)

    fun openDirectory(directory: File)
}

object RealDesktopActions : DesktopActions {
    override fun isDesktopSupported(): Boolean = Desktop.isDesktopSupported()

    override fun open(file: File) {
        Desktop.getDesktop().open(file)
    }

    override fun openDirectory(directory: File) {
        Desktop.getDesktop().open(directory)
    }
}

/** File delete hook for uninstall; tests can simulate partial delete failure. */
fun interface FileDeleter {
    fun delete(file: File): Boolean
}

object RealFileDeleter : FileDeleter {
    override fun delete(file: File): Boolean = file.delete()
}
