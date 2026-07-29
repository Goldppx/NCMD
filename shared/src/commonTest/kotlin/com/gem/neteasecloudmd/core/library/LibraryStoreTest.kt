package com.gem.neteasecloudmd.core.library

import com.gem.neteasecloudmd.core.model.Track
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LibraryStoreTest {
    private val tracks = listOf(
        Track(1, "Graduation", "BrAnTB", "Graduation", null),
        Track(2, "Rain", "SASIOVERLXRD", "Night", null)
    )

    @Test
    fun searchFiltersTheSharedCatalog() {
        val store = LibraryStore(tracks)

        store.navigate(LibraryDestination.SEARCH)
        store.updateQuery("rain")

        assertEquals(listOf(tracks[1]), store.state.value.visibleTracks)
    }

    @Test
    fun selectingTrackUpdatesTheSharedPlaybackState() {
        val store = LibraryStore(tracks)

        store.selectTrack(2)

        assertEquals(1, store.state.value.playback.queue.currentIndex)
        assertTrue(store.state.value.playback.isPlaying)
    }

    @Test
    fun playbackCannotStartWithoutATrack() {
        val store = LibraryStore(emptyList())

        store.togglePlayback()

        assertFalse(store.state.value.playback.isPlaying)
    }
}
