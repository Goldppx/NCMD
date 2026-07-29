package com.gem.neteasecloudmd.core.lyrics

import kotlin.test.Test
import kotlin.test.assertEquals

class LyricParserTest {
    @Test
    fun parsesTheFirstTimestampOnALine() {
        val result = LyricParser.parse("[00:01.2][00:03.045]Hello")

        assertEquals(listOf(LyricLine(1_002, "[00:03.045]Hello")), result)
    }
}
