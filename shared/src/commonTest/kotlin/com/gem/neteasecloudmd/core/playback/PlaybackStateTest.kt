package com.gem.neteasecloudmd.core.playback

import com.gem.neteasecloudmd.core.model.Track
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackStateTest {
    @Test
    fun exposesTheTrackSelectedByTheQueue() {
        val first = Track(1, "First", "Artist", "Album", null)
        val second = Track(2, "Second", "Artist", "Album", null)
        val state = PlaybackState(queue = QueueState(items = listOf(first, second), currentIndex = 1))

        assertEquals(second, state.currentTrack)
    }
}
