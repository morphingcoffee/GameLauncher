package com.morphingcoffee.gamelauncher.golden

import org.junit.Assume

/**
 * Canonical pixel-golden host.
 *
 * Today: macOS arm64 (`macos-15` CI) — matches a supported [com.morphingcoffee.gamelauncher.core.model.PlatformKey]
 * and one Skia rasterizer.
 *
 * Future Linux switch: change [CanonicalGoldenHost], `.github/workflows/ui-goldens.yml` `runs-on`,
 * regenerate all PNGs on that host, and preferably inject platform into composition so the UI tree
 * no longer depends on [com.morphingcoffee.gamelauncher.core.model.PlatformKey.current].
 */
internal object CanonicalGoldenHost {
    const val OS_FAMILY = "mac"
    const val ARCH = "arm64"
    const val CI_RUNNER = "macos-15"
    const val LABEL = "macOS arm64 ($CI_RUNNER)"

    fun matchesCurrentMachine(): Boolean {
        val os =
            System
                .getProperty("os.name")
                .lowercase()
        val arch =
            System
                .getProperty("os.arch")
                .lowercase()
        val osOk = OS_FAMILY == "mac" && ("mac" in os || "darwin" in os)
        val archOk =
            when (ARCH) {
                "arm64" -> arch == "aarch64" || arch == "arm64"
                "x64" -> arch == "x86_64" || arch == "amd64"
                else -> arch == ARCH
            }
        return osOk && archOk
    }
}

internal fun assumeCanonicalGoldenHost() {
    Assume.assumeTrue(
        "UI goldens require ${CanonicalGoldenHost.LABEL}; skipped on this host",
        CanonicalGoldenHost.matchesCurrentMachine(),
    )
}
