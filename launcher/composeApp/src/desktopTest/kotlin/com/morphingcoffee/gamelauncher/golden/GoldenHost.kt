package com.morphingcoffee.gamelauncher.golden

import org.junit.Assume

/**
 * Pixel goldens are canonical on macOS arm64 (`macos-15` CI).
 *
 * Host OS still affects catalog availability UI because [com.morphingcoffee.gamelauncher.core.model.PlatformKey.current]
 * is read inside composition / model helpers — Linux returns null and changes the tree.
 * Skia text rasterization can also differ across OS even with bundled fonts.
 */
internal fun assumeCanonicalGoldenHost() {
    val os =
        System
            .getProperty("os.name")
            .lowercase()
    val arch =
        System
            .getProperty("os.arch")
            .lowercase()
    val isMacArm =
        ("mac" in os || "darwin" in os) &&
            (arch == "aarch64" || arch == "arm64")
    Assume.assumeTrue(
        "UI goldens require macOS arm64 (canonical runner: macos-15); skipped on this host",
        isMacArm,
    )
}
