package com.gem.neteasecloudmd.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricParserTest {

    @Test
    fun parse_null_returnsEmpty() {
        assertTrue(LyricParser.parse(null).isEmpty())
    }

    @Test
    fun parse_empty_returnsEmpty() {
        assertTrue(LyricParser.parse("").isEmpty())
        assertTrue(LyricParser.parse("   ").isEmpty())
    }

    @Test
    fun parse_standardFormat_returnsCorrectLines() {
        val lrc = "[00:01.50]Hello\n[00:05.00]World"
        val result = LyricParser.parse(lrc)
        assertEquals(2, result.size)
        assertEquals(LyricLine(1050, "Hello"), result[0])
        assertEquals(LyricLine(5000, "World"), result[1])
    }

    @Test
    fun parse_msWithThreeDigits_parsesCorrectMs() {
        val lrc = "[01:02.345]Test"
        val result = LyricParser.parse(lrc)
        assertEquals(1, result.size)
        assertEquals(62345, result[0].time)
    }

    @Test
    fun parse_singleDigitMinute_parsesCorrectly() {
        val lrc = "[1:30.00]One minute thirty"
        val result = LyricParser.parse(lrc)
        assertEquals(1, result.size)
        assertEquals(90000, result[0].time)
    }

    @Test
    fun parse_zeroMs_parsesCorrectly() {
        val lrc = "[00:00.00]Start"
        val result = LyricParser.parse(lrc)
        assertEquals(1, result.size)
        assertEquals(0, result[0].time)
        assertEquals("Start", result[0].text)
    }

    @Test
    fun parse_emptyTextAfterTimestamp_omitsLine() {
        val lrc = "[00:01.00]Visible\n[00:02.00]   \n[00:03.00]Also visible"
        val result = LyricParser.parse(lrc)
        assertEquals(2, result.size)
        assertEquals(1000, result[0].time)
        assertEquals(3000, result[1].time)
    }

    @Test
    fun parse_noTimestamp_ignoresLine() {
        val lrc = "This line has no timestamp\n[00:01.00]Valid"
        val result = LyricParser.parse(lrc)
        assertEquals(1, result.size)
        assertEquals(1000, result[0].time)
    }

    @Test
    fun parse_malformedTimestamp_ignoresLine() {
        val lrc = "[abc]no digits\n[00:01.00]Valid"
        val result = LyricParser.parse(lrc)
        assertEquals(1, result.size)
        assertEquals(1000, result[0].time)
    }

    @Test
    fun parse_multipleTimestampsOnLine_findsOnlyFirstMatch() {
        val lrc = "[00:01.00][00:02.00]Dual timestamp"
        val result = LyricParser.parse(lrc)
        assertEquals(1, result.size)
        assertEquals(1000, result[0].time)
    }

    @Test
    fun parse_hourPrefix_notSupported_ignoresLine() {
        val lrc = "[01:02:03.45]Has hours"
        val result = LyricParser.parse(lrc)
        assertTrue(result.isEmpty())
    }

    @Test
    fun parse_mixedFormat_parsesValidLinesOnly() {
        val lrc = """
            [00:01.00]First
            garbage line
            [00:02.00]Second
            [not a valid timestamp]
            [00:03.00]Third
        """.trimIndent()
        val result = LyricParser.parse(lrc)
        assertEquals(3, result.size)
    }

    @Test
    fun parse_resultIsSortedByTime() {
        val lrc = "[00:03.00]Third\n[00:01.00]First\n[00:02.00]Second"
        val result = LyricParser.parse(lrc)
        assertEquals(3, result.size)
        assertEquals(1000, result[0].time)
        assertEquals(2000, result[1].time)
        assertEquals(3000, result[2].time)
    }

    @Test
    fun parse_twoDigitMilliseconds_interpretedCorrectly() {
        val lrc = "[00:01.50]One second 50ms"
        val result = LyricParser.parse(lrc)
        assertEquals(1050, result[0].time)
    }

    @Test
    fun parse_largeValues_doesNotOverflow() {
        val lrc = "[99:59.999]Large value"
        val result = LyricParser.parse(lrc)
        assertEquals(1, result.size)
        assertEquals(99 * 60_000 + 59 * 1_000 + 999, result[0].time)
    }
}
