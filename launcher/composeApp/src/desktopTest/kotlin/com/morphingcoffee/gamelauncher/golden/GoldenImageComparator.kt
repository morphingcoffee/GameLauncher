package com.morphingcoffee.gamelauncher.golden

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.assertTrue
import kotlin.test.fail

private const val PER_CHANNEL_TOLERANCE = 2
private const val MAX_DIFF_FRACTION = 0.001 // 0.1%

internal object GoldenPaths {
    val goldenDir: File
        get() =
            File(
                System.getProperty("gamelauncher.golden.dir")
                    ?: error("Missing system property gamelauncher.golden.dir"),
            )

    val screenshotRoot: File
        get() =
            File(
                System.getProperty("gamelauncher.screenshot.dir")
                    ?: error("Missing system property gamelauncher.screenshot.dir"),
            )

    val actualDir: File
        get() = screenshotRoot.resolve("actual")

    val diffDir: File
        get() = screenshotRoot.resolve("diff")

    val updateGolden: Boolean
        get() =
            System
                .getProperty("gamelauncher.updateGolden", "false")
                .equals("true", ignoreCase = true)
}

internal fun assertMatchesGolden(
    name: String,
    actual: BufferedImage,
) {
    GoldenPaths.actualDir.mkdirs()
    GoldenPaths.diffDir.mkdirs()
    GoldenPaths.goldenDir.mkdirs()

    val actualFile = GoldenPaths.actualDir.resolve("$name.png")
    ImageIO.write(actual, "png", actualFile)

    val expectedFile = GoldenPaths.goldenDir.resolve("$name.png")
    if (GoldenPaths.updateGolden) {
        ImageIO.write(actual, "png", expectedFile)
        return
    }

    if (!expectedFile.isFile) {
        fail(
            "Missing golden baseline for '$name' at ${expectedFile.absolutePath}.\n" +
                "Generate on the canonical host (macOS arm64 / GitHub Actions macos-15):\n" +
                "  ./gradlew -p launcher :composeApp:desktopTest -PupdateGolden\n" +
                "or dispatch the ui-goldens workflow with update_golden=true and commit the " +
                "downloaded PNGs under launcher/composeApp/screenshots/golden/.\n" +
                "Actual image written to ${actualFile.absolutePath}",
        )
    }

    val expected =
        ImageIO.read(expectedFile)
            ?: fail("Failed to read golden baseline: ${expectedFile.absolutePath}")

    if (expected.width != actual.width || expected.height != actual.height) {
        fail(
            "Golden size mismatch for '$name': " +
                "expected ${expected.width}x${expected.height}, " +
                "actual ${actual.width}x${actual.height}.\n" +
                "expected=${expectedFile.absolutePath}\n" +
                "actual=${actualFile.absolutePath}",
        )
    }

    val totalPixels = expected.width.toLong() * expected.height.toLong()
    val diffImage =
        BufferedImage(expected.width, expected.height, BufferedImage.TYPE_INT_ARGB)
    var differing = 0L

    for (y in 0 until expected.height) {
        for (x in 0 until expected.width) {
            val e = expected.getRGB(x, y)
            val a = actual.getRGB(x, y)
            if (pixelsDiffer(e, a)) {
                differing++
                diffImage.setRGB(x, y, 0xFFFF0000.toInt())
            } else {
                // Dim expected pixel so the red highlights stay readable.
                val dim =
                    (e and 0x00FFFFFF) or
                        (((e ushr 24) and 0xFF) / 3 shl 24)
                diffImage.setRGB(x, y, dim)
            }
        }
    }

    val fraction = differing.toDouble() / totalPixels.toDouble()
    if (fraction > MAX_DIFF_FRACTION) {
        val diffFile = GoldenPaths.diffDir.resolve("$name.png")
        ImageIO.write(diffImage, "png", diffFile)
        fail(
            "Golden mismatch for '$name': " +
                "$differing / $totalPixels pixels differ " +
                "(${"%.4f".format(fraction * 100.0)}% > ${MAX_DIFF_FRACTION * 100.0}%).\n" +
                "expected=${expectedFile.absolutePath}\n" +
                "actual=${actualFile.absolutePath}\n" +
                "diff=${diffFile.absolutePath}",
        )
    }

    assertTrue(true)
}

private fun pixelsDiffer(
    expectedArgb: Int,
    actualArgb: Int,
): Boolean {
    val ea = (expectedArgb ushr 24) and 0xFF
    val er = (expectedArgb ushr 16) and 0xFF
    val eg = (expectedArgb ushr 8) and 0xFF
    val eb = expectedArgb and 0xFF
    val aa = (actualArgb ushr 24) and 0xFF
    val ar = (actualArgb ushr 16) and 0xFF
    val ag = (actualArgb ushr 8) and 0xFF
    val ab = actualArgb and 0xFF
    return abs(ea - aa) > PER_CHANNEL_TOLERANCE ||
        abs(er - ar) > PER_CHANNEL_TOLERANCE ||
        abs(eg - ag) > PER_CHANNEL_TOLERANCE ||
        abs(eb - ab) > PER_CHANNEL_TOLERANCE
}
