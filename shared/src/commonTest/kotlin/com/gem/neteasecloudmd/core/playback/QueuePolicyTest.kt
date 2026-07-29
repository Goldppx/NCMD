package com.gem.neteasecloudmd.core.playback

import kotlin.test.Test
import kotlin.test.assertEquals

class QueuePolicyTest {
    @Test
    fun removingTheCurrentLastItemSelectsTheNewLastItem() {
        val state = QueueState(items = listOf("first", "second"), currentIndex = 1)

        assertEquals(QueueState(items = listOf("first"), currentIndex = 0), QueuePolicy.afterRemoval(state, 1))
    }
}
