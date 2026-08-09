package com.morphingcoffee.gamelauncher

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WindowIconResourceTest {
    @Test
    fun windowIconPngIsOnDesktopClasspath() {
        val stream =
            Thread.currentThread().contextClassLoader.getResourceAsStream("window-icon.png")
                ?: WindowIconResourceTest::class.java.getResourceAsStream("/window-icon.png")
        assertNotNull(stream, "window-icon.png missing from desktop classpath resources")
        stream.use { bytes ->
            val header = ByteArray(8)
            assertTrue(bytes.read(header) == 8, "window-icon.png is empty")
            assertTrue(
                header contentEquals
                    byteArrayOf(
                        0x89.toByte(),
                        0x50,
                        0x4e,
                        0x47,
                        0x0d,
                        0x0a,
                        0x1a,
                        0x0a,
                    ),
                "window-icon.png is not a PNG",
            )
        }
    }
}
