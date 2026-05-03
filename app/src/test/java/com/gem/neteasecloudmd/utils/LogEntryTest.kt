package com.gem.neteasecloudmd.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LogEntryTest {

    @Test
    fun format_containsAllFields() {
        val entry = LogEntry(
            timestamp = 0L,
            level = LogLevel.INFO,
            tag = "TestTag",
            message = "Test message"
        )
        val formatted = entry.format()
        assertTrue(formatted.contains("[INFO]"))
        assertTrue(formatted.contains("TestTag:"))
        assertTrue(formatted.contains("Test message"))
    }

    @Test
    fun format_includesTimestamp() {
        val entry = LogEntry(
            timestamp = 1700000000000L,
            level = LogLevel.WARN,
            tag = "T",
            message = "M"
        )
        val formatted = entry.format()
        assertTrue(formatted.matches(Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3} \[WARN\] T: M""")))
    }

    @Test
    fun format_debugLevel() {
        val entry = LogEntry(0L, LogLevel.DEBUG, "tag", "msg")
        val formatted = entry.format()
        assertTrue(formatted.contains("[DEBUG]"))
    }

    @Test
    fun format_errorLevel() {
        val entry = LogEntry(0L, LogLevel.ERROR, "tag", "msg")
        val formatted = entry.format()
        assertTrue(formatted.contains("[ERROR]"))
    }

    @Test
    fun format_emptyMessage() {
        val entry = LogEntry(0L, LogLevel.INFO, "tag", "")
        assertEquals("", entry.format().split(": ").getOrNull(1))
    }

    @Test
    fun format_emptyTag() {
        val entry = LogEntry(0L, LogLevel.INFO, "", "message")
        val formatted = entry.format()
        assertTrue(formatted.contains(": message"))
    }

    @Test
    fun format_longMessage() {
        val longMsg = "A".repeat(10_000)
        val entry = LogEntry(0L, LogLevel.INFO, "tag", longMsg)
        val formatted = entry.format()
        assertTrue(formatted.contains(longMsg))
    }

    @Test
    fun logLevel_enumValues() {
        assertEquals(4, LogLevel.values().size)
        assertEquals(LogLevel.DEBUG, LogLevel.valueOf("DEBUG"))
        assertEquals(LogLevel.INFO, LogLevel.valueOf("INFO"))
        assertEquals(LogLevel.WARN, LogLevel.valueOf("WARN"))
        assertEquals(LogLevel.ERROR, LogLevel.valueOf("ERROR"))
    }

    @Test
    fun logLevel_ordinalOrder() {
        assertTrue(LogLevel.DEBUG.ordinal < LogLevel.INFO.ordinal)
        assertTrue(LogLevel.INFO.ordinal < LogLevel.WARN.ordinal)
        assertTrue(LogLevel.WARN.ordinal < LogLevel.ERROR.ordinal)
    }
}
