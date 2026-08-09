package com.morphingcoffee.gamelauncher.core.telemetry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProcessOutputBufferTest {
    @Test
    fun retainsOnlyBoundedTail() {
        val buffer = ProcessOutputBuffer(maxChars = 10)
        buffer.append("abcdefghij")
        buffer.append("XYZ")
        assertEquals("defghijXYZ", buffer.snapshot())
    }

    @Test
    fun appendLineAddsNewline() {
        val buffer = ProcessOutputBuffer()
        buffer.appendLine("hello")
        assertTrue(buffer.snapshot().endsWith("hello\n"))
    }
}
