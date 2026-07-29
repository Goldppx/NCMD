package com.gem.neteasecloudmd.core.playback

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackRequestPolicyTest {
    private val prefetchedUrl = PrefetchedUrl(
        queueRevision = 7,
        trackId = 42,
        url = "https://example.com/track.mp3"
    )

    @Test
    fun acceptsAUrlForTheSameQueueRevisionAndTrack() {
        assertTrue(PlaybackRequestPolicy.canUsePrefetchedUrl(prefetchedUrl, 7, 42))
    }

    @Test
    fun rejectsAUrlForAnOutdatedQueueOrDifferentTrack() {
        assertFalse(PlaybackRequestPolicy.canUsePrefetchedUrl(prefetchedUrl, 8, 42))
        assertFalse(PlaybackRequestPolicy.canUsePrefetchedUrl(prefetchedUrl, 7, 99))
    }
}
